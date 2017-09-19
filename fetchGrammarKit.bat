@Echo Off
:: call ant tasks that downloads and extracts grammarKit
call ant -f prepare.xml makeToolsDir grammarKit.fetch grammarKit.extract psi.fetch  psi.extract cleanup