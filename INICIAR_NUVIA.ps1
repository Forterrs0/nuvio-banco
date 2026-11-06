# Iniciador local: recebe senha sem ecoar caracteres no terminal.
# Nao salva credenciais em arquivo. Nao faz deploy.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not $env:NUVIA_DB_USER) { $env:NUVIA_DB_USER = 'nuvia_app' }
if (-not $env:NUVIA_DB_HOST) { $env:NUVIA_DB_HOST = 'localhost' }
if (-not $env:NUVIA_DB_NAME) { $env:NUVIA_DB_NAME = 'nuvia_db' }

$providedPassword = [bool]$env:NUVIA_DB_PASSWORD
if (-not $providedPassword) {
    $secure = Read-Host "Senha MySQL do usuario '$($env:NUVIA_DB_USER)'" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $env:NUVIA_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}
try {
    Write-Host ''
    Write-Host "Conectando em $($env:NUVIA_DB_HOST):3306/$($env:NUVIA_DB_NAME) como $($env:NUVIA_DB_USER)..."
    Write-Host 'Quando iniciar, abra http://localhost:8080'
    Write-Host 'Pressione Ctrl+C para encerrar.'
    & mvn spring-boot:run
    $code = $LASTEXITCODE
} finally {
    if (-not $providedPassword) { Remove-Item Env:NUVIA_DB_PASSWORD -ErrorAction SilentlyContinue }
}
exit $code
