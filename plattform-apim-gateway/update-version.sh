#!/bin/bash

# exit if error
set -e

OLD_VERSION=$1
NEW_VERSION=$2

if [ -z "${OLD_VERSION}" ] || [ -z "${NEW_VERSION}" ]
then
    echo "Usage: update-version.sh <old-version> <new-version>"
    echo "-> example update-version.sh 1.3.0 1.4.0"
    exit 1
fi

read -p "Updating version ${OLD_VERSION} to ${NEW_VERSION} - continue? (y|n)" -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    # 1.4.0 -> 1\.4\.0 (regex pattern)
    OLD_VERSION_ESCAPED=$(echo "$OLD_VERSION" | sed 's/\./\\./g')
    NEW_VERSION_ESCAPED=$(echo "$NEW_VERSION" | sed 's/\./\\./g')

    # 1.4.0 -> 1-4-0
    OLD_VERSION_DASHED=$(echo "$OLD_VERSION" | sed 's/\./-/g')
    NEW_VERSION_DASHED=$(echo "$NEW_VERSION" | sed 's/\./-/g')
    echo "- OLD_VERSION = $OLD_VERSION ($OLD_VERSION_DASHED)"
    echo "- NEW_VERSION = $NEW_VERSION ($NEW_VERSION_DASHED)"

    echo "going to rename openshift template files:"
    echo "---"
    find openshift/templates -type f -wholename *.yml | while read filename
    do
        newfilename="$(echo "$filename" | sed s/$OLD_VERSION/$NEW_VERSION/)"
        echo "rename $filename to $newfilename"
        git mv $filename $newfilename
    done

    echo "---"
    echo ""
    find openshift/templates -type f -wholename *.yml | while read filename
    do
        echo "replace versions in file $filename"
        sed -i -e "s/${OLD_VERSION_ESCAPED}/${NEW_VERSION_ESCAPED}/g" $filename
        sed -i -e "s/${OLD_VERSION_DASHED}/${NEW_VERSION_DASHED}/g" $filename
    done

    # cleanup sed tmp files -> I wasn't able to skip those on windows cmder shell sed version, thus just kick them out
    find openshift/templates -type f -wholename *sed* | xargs rm

    echo "---"
    echo "done."
    echo "IMPORTANT: please update the pom version by yourself to $NEW_VERSION-SNAPSHOT"
    echo "---"

else
    echo "aborted"
fi