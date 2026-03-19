param(
  [string]$Url = 'http://localhost:8080/api/v1/jobs'
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

function Run-Load {
  param(
    [string]$TargetUrl,
    [int]$Users,
    [int]$ReqPerUser
  )

  $jobs = @()
  $allStart = [System.Diagnostics.Stopwatch]::StartNew()

  1..$Users | ForEach-Object {
    $userId = $_
    $jobs += Start-Job -ScriptBlock {
      param($TargetUrl, $ReqPerUser, $UserId)

      $results = @()
      for ($i = 1; $i -le $ReqPerUser; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $status = 0

        try {
          $response = Invoke-WebRequest -Uri $TargetUrl -Method GET -TimeoutSec 30
          $status = [int]$response.StatusCode
        }
        catch {
          $status = 0
        }

        $sw.Stop()
        $results += [pscustomobject]@{
          user   = $UserId
          status = $status
          ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
        }
      }

      return $results
    } -ArgumentList $TargetUrl, $ReqPerUser, $userId
  }

  Wait-Job -Job $jobs | Out-Null
  $all = Receive-Job -Job $jobs
  Remove-Job -Job $jobs -Force

  $allStart.Stop()

  $total = $all.Count
  $ok = ($all | Where-Object { $_.status -ge 200 -and $_.status -lt 400 }).Count
  $err = $total - $ok
  $times = [double[]]($all | ForEach-Object { [double]$_.ms })

  $avg = [math]::Round((($times | Measure-Object -Average).Average), 2)
  $p95 = [math]::Round((Get-Percentile -Values $times -P 0.95), 2)
  $p99 = [math]::Round((Get-Percentile -Values $times -P 0.99), 2)
  $max = [math]::Round((($times | Measure-Object -Maximum).Maximum), 2)

  $duration = [math]::Max(0.001, [math]::Round($allStart.Elapsed.TotalSeconds, 3))
  $rps = [math]::Round($total / $duration, 2)

  return [pscustomobject]@{
    users          = $Users
    reqPerUser     = $ReqPerUser
    totalRequests  = $total
    success        = $ok
    errors         = $err
    successRatePct = [math]::Round((100.0 * $ok / $total), 2)
    avgMs          = $avg
    p95Ms          = $p95
    p99Ms          = $p99
    maxMs          = $max
    durationSec    = $duration
    throughputRps  = $rps
  }
}

$scenarios = @(
  @{ Users = 20; ReqPerUser = 10 },
  @{ Users = 50; ReqPerUser = 8 },
  @{ Users = 100; ReqPerUser = 5 }
)

$results = @()
foreach ($s in $scenarios) {
  $results += Run-Load -TargetUrl $Url -Users $s.Users -ReqPerUser $s.ReqPerUser
}

$results | ConvertTo-Json -Depth 4
