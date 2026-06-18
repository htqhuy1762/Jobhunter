$ErrorActionPreference = 'Stop'

$connectUrl = $env:KAFKA_CONNECT_URL
if ([string]::IsNullOrWhiteSpace($connectUrl)) {
    $connectUrl = 'http://localhost:8088'
}

$connectorName = 'job-db-jobs-connector'
$configPath = Join-Path $PSScriptRoot '..\docker\debezium\job-db-jobs-connector.json'
$configPath = [System.IO.Path]::GetFullPath($configPath)

if (-not (Test-Path $configPath)) {
    throw "Connector config not found: $configPath"
}

$configBody = Get-Content -Raw -Path $configPath

try {
    $existing = Invoke-RestMethod -Method Get -Uri "$connectUrl/connectors/$connectorName"
    if ($null -ne $existing) {
        Write-Host "Connector already exists. Updating config..."
        $parsed = $configBody | ConvertFrom-Json
        $parsed.config | ConvertTo-Json -Depth 20 | Out-File -FilePath "$env:TEMP\connector-config.json" -Encoding utf8
        Invoke-RestMethod -Method Put -Uri "$connectUrl/connectors/$connectorName/config" -ContentType 'application/json' -InFile "$env:TEMP\connector-config.json"
        Write-Host "Connector updated: $connectorName"
        exit 0
    }
} catch {
    Write-Host "Connector not found. Creating new connector..."
}

Invoke-RestMethod -Method Post -Uri "$connectUrl/connectors" -ContentType 'application/json' -Body $configBody
Write-Host "Connector created: $connectorName"

