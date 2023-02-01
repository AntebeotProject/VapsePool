$pathToBitcoinTypeCoins = @("C:\dogecoin-1.14.6\bin\dogecoin-qt.exe")
$gostcoinPath = @("C:\gostcoin-qt.exe")
$progForBatsNames = @("monero-wallet-rpc", "electrum-4.3.3-debug")
$pathToRunBats = @("C:\Program Files\Monero GUI Wallet\START RPC SERVER.bat", "C:\Program Files (x86)\Electrum\RUN.bat")
<#
    Запускает процессы, если таковы ещё не запущены.
    Передающиеся параметры должны быть массивом строковым. @("-testnet") как пример.
    В случае если процесс запущен - сообщает об этом, без запуска программ.
#>

function runElectrum ($path, $params)
{
    if ( !(Get-Process electrum-4.3.3-debug -ErrorAction SilentlyContinue) ) {
        Start-Process -FilePath 'C:\Program Files (x86)\Electrum\electrum-4.3.3-debug.exe' -ArgumentList $params
    }
}
runElectrum 'C:\Program Files (x86)\Electrum\electrum-4.3.3-debug.exe' "--testnet"

function runWithParameter($arr, $params)
{
    foreach ($prog in $arr)
    {
        #echo $prog
        $name_ = ($prog.split("\\")[-1]) #
        $name = $name_.SubString(0, $name_.LastIndexOf('.'))
        #echo $name
        if(Get-Process $name -ErrorAction SilentlyContinue) {
         echo "$name runned already. ignore" 
         } else {
          echo "$name ($prog) is not runned"
          echo "Run $prog $params"
          Start-Process -FilePath $prog -ArgumentList $params
        }
    }
}
<#
 Запускает batch файл, если процесс, который указывается первым аргументом. без .exe запущен в системе.
 в противном случае сообщает что процесс уже запущен
#>
function runBatIfNotExistsProc($procName, $batPath)
{
    if ( !(Get-Process $procName -ErrorAction SilentlyContinue) ) {
        echo "$procName not runed. run $batPath"
        $workDir =  $batPath.SubString(0, $batPath.LastIndexOf('\'))
        Start-Process -FilePath $batPath -WorkingDirectory $workDir
    } else {
        echo "$procName runnedAlready"
    }
}
runWithParameter $pathToBitcoinTypeCoins "-testnet"

$i = 0
while ($i -lt $progForBatsNames.Length)
{
    runBatIfNotExistsProc $progForBatsNames[$i] $pathToRunBats[$i]
    $i = $i + 1
}


# .\gostcoin-qt.exe -datadir="C:\poolserver\BlockChains\Gostcoin" -testnet
if ( !(Get-Process gostcoin-qt -ErrorAction SilentlyContinue) ) {
    Start-Process -FilePath "C:\gostcoin-qt.exe" -WorkingDirectory "C:\poolserver\BlockChains\Gostcoin" -ArgumentList @("-testnet", '-datadir="C:\poolserver\BlockChains\Gostcoin"')
} else 
{
    echo "Gostcoin was run already"
}