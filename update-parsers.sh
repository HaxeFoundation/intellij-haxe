#!/bin/bash

# Read license from IDEA project settings
LICENSE_FILE=.idea/copyright/apache_2_license.xml
year=`date +%Y`
license=`grep -oP '(?<=option name="notice" value=")[^"]*(?=")' $LICENSE_FILE`
license="${license//&#10;&#10;/
 *
 * }"
license="${license//&#10;/
 * }"
license=`echo "$license" | recode xml..ascii`
license="${license/&#36;today.year/$year}"
license="/*
 * $license
 */
"

# Add copyrights to generated files
find ./gen -type f -name "*.java" -print0 | while IFS= read -r -d $'\0' file; do
    first_line=`head -n 1 "$file"`
    if [ "$first_line" != "/*" ]; then
        tmp_name="$file~"
        echo "$license" | cat - "$file" > "$tmp_name" && mv $tmp_name $file
    fi
done
