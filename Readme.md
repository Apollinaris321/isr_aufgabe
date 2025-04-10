Das Programm besteht aus der Hauptmethode "importFiles" die zwei Pfade nimmt, einmal "sourceDir" und "targetDir". 
In unserem Fall wäre "sourceDir"=input und "targetDir"=import

Die Dateiein werden erst eingelesen mithilfe von readFileNames und dann wird mit
cleanFileName der Name der Datei bereinigt und unerlaubte Zeichen durch ein Underscore ersetzt.
Anschließend wird die Datei in das TargetDir gelegt. 

Die executable Jar kann in den Ordner gelegt werden und muss sich auf gleicher Höhe mit "input" und "import" befinden.

folder
    - input
    - import
    - executable_jar