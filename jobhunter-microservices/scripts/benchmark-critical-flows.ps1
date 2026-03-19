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

function Run-Login-Benchmark {
  param(
    [string]$BaseUrl,
    [int]$Users,
    [int]$ReqPerUser,
    [string]$Username,
    [string]$Password
  )

  $jobs = @()
  $allSw = [System.Diagnostics.Stopwatch]::StartNew()

  1..$Users | ForEach-Object {
    $jobs += Start-Job -ScriptBlock {
      param($BaseUrl, $ReqPerUser, $Username, $Password)

      $results = @()
      $url = "$BaseUrl/api/v1/auth/login"
      $body = @{ username = $Username; password = $Password } | ConvertTo-Json

      for ($i = 1; $i -le $ReqPerUser; $i++) {
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
        $results += [pscustomobject]@{
          status = $status
          ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
        }
      }

      return $results
    } -ArgumentList $BaseUrl, $ReqPerUser, $Username, $Password
  }

  Wait-Job -Job $jobs | Out-Null
  $all = Receive-Job -Job $jobs
  Remove-Job -Job $jobs -Force
  $allSw.Stop()

  return Summarize-Results -Scenario 'login' -Users $Users -ReqPerUser $ReqPerUser -Items $all -DurationSec $allSw.Elapsed.TotalSeconds
}

function Run-Resume-Apply-Benchmark {
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
  $allSw = [System.Diagnostics.Stopwatch]::StartNew()

  1..$Users | ForEach-Object {
    $userNum = $_
    $jobs += Start-Job -ScriptBlock {
      param($BaseUrl, $ReqPerUser, $Token, $Email, $UserId, $JobId, $UserNum)

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
        $results += [pscustomobject]@{
          status = $status
          ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
        }
      }

      return $results
    } -ArgumentList $BaseUrl, $ReqPerUser, $Token, $Email, $UserId, $JobId, $userNum
  }

  Wait-Job -Job $jobs | Out-Null
  $all = Receive-Job -Job $jobs
  Remove-Job -Job $jobs -Force
  $allSw.Stop()

  return Summarize-Results -Scenario 'apply_resume' -Users $Users -ReqPerUser $ReqPerUser -Items $all -DurationSec $allSw.Elapsed.TotalSeconds
}

# Bootstrap auth token + user/job references
$loginBody = @{ username = 'user@gmail.com'; password = '123456' } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method POST -ContentType 'application/json' -Body $loginBody
$token = $loginResp.data.access_token
if (-not $token) {
  throw 'Unable to obtain access token from login response'
}

$userId = [long]$loginResp.data.user.id
$email = [string]$loginResp.data.user.email
$jobsResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/jobs?page=1&size=1" -Method GET
$jobId = [long]$jobsResp.data.result[0].id

$results = @()
$results += Run-Login-Benchmark -BaseUrl $BaseUrl -Users 20 -ReqPerUser 5 -Username 'user@gmail.com' -Password '123456'
$results += Run-Resume-Apply-Benchmark -BaseUrl $BaseUrl -Users 20 -ReqPerUser 5 -Token $token -Email $email -UserId $userId -JobId $jobId

$results | ConvertTo-Json -Depth 5
