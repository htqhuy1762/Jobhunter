param(
  [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

function Get-Percentile {
  param(
    [double[]]$Values,
    [double]$P
  )

  if (-not $Values -or $Values.Count -eq 0) {
    return 0
  }

  $sorted = $Values | Sort-Object
  $idx = [math]::Ceiling($P * $sorted.Count) - 1
  if ($idx -lt 0) { $idx = 0 }
  if ($idx -ge $sorted.Count) { $idx = $sorted.Count - 1 }
  return [double]$sorted[$idx]
}

function Get-Median {
  param([double[]]$Values)

  if (-not $Values -or $Values.Count -eq 0) {
    return 0
  }

  $sorted = $Values | Sort-Object
  $n = $sorted.Count
  if ($n % 2 -eq 1) {
    return [double]$sorted[[int][math]::Floor($n / 2)]
  }

  $a = [double]$sorted[($n / 2) - 1]
  $b = [double]$sorted[$n / 2]
  return ($a + $b) / 2.0
}

function Summarize-Results {
  param(
    [string]$Scenario,
    [int]$Users,
    [int]$ReqPerUser,
    [array]$Items,
    [double]$DurationSec
  )

  $total = $Items.Count
  $ok = ($Items | Where-Object { $_.status -ge 200 -and $_.status -lt 400 }).Count
  $err = $total - $ok
  $times = [double[]]($Items | ForEach-Object { [double]$_.ms })

  $avg = [math]::Round((($times | Measure-Object -Average).Average), 2)
  $p95 = [math]::Round((Get-Percentile -Values $times -P 0.95), 2)
  $p99 = [math]::Round((Get-Percentile -Values $times -P 0.99), 2)
  $max = [math]::Round((($times | Measure-Object -Maximum).Maximum), 2)
  $rps = [math]::Round($total / [math]::Max(0.001, $DurationSec), 2)

  return [pscustomobject]@{
    scenario       = $Scenario
    users          = $Users
    reqPerUser     = $ReqPerUser
    totalRequests  = $total
    success        = $ok
    errors         = $err
    successRatePct = [math]::Round((100.0 * $ok / [math]::Max(1, $total)), 2)
    avgMs          = $avg
    p95Ms          = $p95
    p99Ms          = $p99
    maxMs          = $max
    durationSec    = [math]::Round($DurationSec, 3)
    throughputRps  = $rps
  }
}

function Run-Login-Sequential {
  param(
    [string]$BaseUrl,
    [int]$Requests,
    [string]$Username,
    [string]$Password
  )

  $url = "$BaseUrl/api/v1/auth/login"
  $body = @{ username = $Username; password = $Password } | ConvertTo-Json
  $items = @()
  $swAll = [System.Diagnostics.Stopwatch]::StartNew()

  for ($i = 1; $i -le $Requests; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = 0

    try {
      $resp = Invoke-WebRequest -Uri $url -Method POST -ContentType 'application/json' -Body $body -TimeoutSec 30
      $status = [int]$resp.StatusCode
    }
    catch {
      if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
        $status = [int]$_.Exception.Response.StatusCode.value__
      }
    }

    $sw.Stop()
    $items += [pscustomobject]@{ status = $status; ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 2) }
  }

  $swAll.Stop()
  return Summarize-Results -Scenario 'login_safe' -Users 1 -ReqPerUser $Requests -Items $items -DurationSec $swAll.Elapsed.TotalSeconds
}

function Run-Apply-Concurrent {
  param(
    [string]$BaseUrl,
    [int]$Users,
    [int]$ReqPerUser,
    [string]$Token,
    [string]$Email,
    [long]$UserId,
    [long]$JobId
  )

  $jobs = @()
  $swAll = [System.Diagnostics.Stopwatch]::StartNew()

  1..$Users | ForEach-Object {
    $jobs += Start-Job -ScriptBlock {
      param($BaseUrl, $ReqPerUser, $Token, $Email, $UserId, $JobId)

      $results = @()
      $url = "$BaseUrl/api/v1/resumes"
      $headers = @{ Authorization = "Bearer $Token" }

      for ($i = 1; $i -le $ReqPerUser; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $status = 0

        $payload = @{
          email  = $Email
          url    = "https://cdn.example.com/cv/$([guid]::NewGuid().ToString()).pdf"
          status = 'PENDING'
          user   = @{ id = $UserId }
          job    = @{ id = $JobId }
        } | ConvertTo-Json -Depth 5

        try {
          $resp = Invoke-WebRequest -Uri $url -Method POST -ContentType 'application/json' -Headers $headers -Body $payload -TimeoutSec 30
          $status = [int]$resp.StatusCode
        }
        catch {
          if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode.value__
          }
        }

        $sw.Stop()
        $results += [pscustomobject]@{ status = $status; ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 2) }
      }

      return $results
    } -ArgumentList $BaseUrl, $ReqPerUser, $Token, $Email, $UserId, $JobId
  }

  Wait-Job -Job $jobs | Out-Null
  $all = Receive-Job -Job $jobs
  Remove-Job -Job $jobs -Force
  $swAll.Stop()

  return Summarize-Results -Scenario 'apply_resume' -Users $Users -ReqPerUser $ReqPerUser -Items $all -DurationSec $swAll.Elapsed.TotalSeconds
}

function Build-Median-Summary {
  param([array]$Runs)

  $byScenario = $Runs | Group-Object scenario
  $summary = @()

  foreach ($g in $byScenario) {
    $items = $g.Group
    $summary += [pscustomobject]@{
      scenario = $g.Name
      medianSuccessRatePct = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.successRatePct }))), 2)
      medianAvgMs = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.avgMs }))), 2)
      medianP95Ms = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.p95Ms }))), 2)
      medianP99Ms = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.p99Ms }))), 2)
      medianThroughputRps = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.throughputRps }))), 2)
      medianErrors = [math]::Round((Get-Median ([double[]]($items | ForEach-Object { $_.errors }))), 2)
    }
  }

  return $summary
}

# Bootstrap auth/token and job reference
$loginBootstrapBody = @{ username = 'user@gmail.com'; password = '123456' } | ConvertTo-Json
$loginBootstrap = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType 'application/json' -Body $loginBootstrapBody
$token = $loginBootstrap.data.access_token
if (-not $token) {
  throw 'Cannot get access token for benchmark bootstrap.'
}
$userId = [long]$loginBootstrap.data.user.id
$email = [string]$loginBootstrap.data.user.email
$jobsResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/jobs?page=1&size=1" -Method GET
$jobId = [long]$jobsResp.data.result[0].id

# Warm-up phase (not included in measured runs)
$null = Run-Login-Sequential -BaseUrl $BaseUrl -Requests 5 -Username 'user@gmail.com' -Password '123456'
$null = Run-Apply-Concurrent -BaseUrl $BaseUrl -Users 10 -ReqPerUser 2 -Token $token -Email $email -UserId $userId -JobId $jobId

# Measured runs (3 rounds)
$allRuns = @()
for ($round = 1; $round -le 3; $round++) {
  $allRuns += (Run-Login-Sequential -BaseUrl $BaseUrl -Requests 5 -Username 'user@gmail.com' -Password '123456' | Add-Member -PassThru NoteProperty round $round)
  $allRuns += (Run-Apply-Concurrent -BaseUrl $BaseUrl -Users 20 -ReqPerUser 5 -Token $token -Email $email -UserId $userId -JobId $jobId | Add-Member -PassThru NoteProperty round $round)
}

$result = [pscustomobject]@{
  measuredRuns = $allRuns
  median = Build-Median-Summary -Runs $allRuns
}

$result | ConvertTo-Json -Depth 6
