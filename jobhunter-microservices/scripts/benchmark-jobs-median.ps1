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

  return (([double]$sorted[$n / 2 - 1]) + ([double]$sorted[$n / 2])) / 2.0
}

function Run-Load {
  param(
    [string]$TargetUrl,
    [int]$Users,
    [int]$ReqPerUser
  )

  $jobs = @()
  $swAll = [System.Diagnostics.Stopwatch]::StartNew()

  1..$Users | ForEach-Object {
    $jobs += Start-Job -ScriptBlock {
      param($TargetUrl, $ReqPerUser)

      $results = @()
      for ($i = 1; $i -le $ReqPerUser; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $status = 0
        try {
          $resp = Invoke-WebRequest -Uri $TargetUrl -Method GET -TimeoutSec 30
          $status = [int]$resp.StatusCode
        }
        catch {
          $status = 0
        }

        $sw.Stop()
        $results += [pscustomobject]@{
          status = $status
          ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
        }
      }

      return $results
    } -ArgumentList $TargetUrl, $ReqPerUser
  }

  Wait-Job -Job $jobs | Out-Null
  $all = Receive-Job -Job $jobs
  Remove-Job -Job $jobs -Force
  $swAll.Stop()

  $times = [double[]]($all | ForEach-Object { [double]$_.ms })
  $total = $all.Count
  $ok = ($all | Where-Object { $_.status -ge 200 -and $_.status -lt 400 }).Count

  return [pscustomobject]@{
    totalRequests = $total
    success = $ok
    errors = $total - $ok
    successRatePct = [math]::Round((100.0 * $ok / $total), 2)
    avgMs = [math]::Round((($times | Measure-Object -Average).Average), 2)
    p95Ms = [math]::Round((Get-Percentile -Values $times -P 0.95), 2)
    p99Ms = [math]::Round((Get-Percentile -Values $times -P 0.99), 2)
    maxMs = [math]::Round((($times | Measure-Object -Maximum).Maximum), 2)
    durationSec = [math]::Round($swAll.Elapsed.TotalSeconds, 3)
    throughputRps = [math]::Round($total / [math]::Max(0.001, $swAll.Elapsed.TotalSeconds), 2)
  }
}

# warm-up
$null = Run-Load -TargetUrl $Url -Users 10 -ReqPerUser 2

$runs = @()
for ($round = 1; $round -le 3; $round++) {
  $run = Run-Load -TargetUrl $Url -Users 100 -ReqPerUser 5
  $run | Add-Member -NotePropertyName round -NotePropertyValue $round -PassThru | ForEach-Object { $runs += $_ }
}

$median = [pscustomobject]@{
  scenario = 'jobs_api_100x5'
  medianSuccessRatePct = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.successRatePct }))), 2)
  medianAvgMs = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.avgMs }))), 2)
  medianP95Ms = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.p95Ms }))), 2)
  medianP99Ms = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.p99Ms }))), 2)
  medianThroughputRps = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.throughputRps }))), 2)
  medianErrors = [math]::Round((Get-Median ([double[]]($runs | ForEach-Object { $_.errors }))), 2)
}

[pscustomobject]@{
  runs = $runs
  median = $median
} | ConvertTo-Json -Depth 6
