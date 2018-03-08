#!/bin/bash

# formats: json, bash, java properties


OUT_DIR=build/resources/main
WEBAPP_DIR=src/main/webapp
LOG=$OUT_DIR/git-log.txt
STATUS=$OUT_DIR/git-status.txt

JSON=$WEBAPP_DIR/git.json
PROPERTIES=$OUT_DIR/git.properties
BASH=$OUT_DIR/git.sh
VERSH=$OUT_DIR/sversion.txt
VERSH2=$OUT_DIR/sversion2.txt

mkdir -p $OUT_DIR
mkdir -p $WEBAPP_DIR
git log | head -4 > $LOG
git status > $STATUS
git fetch --tags

DISPLAY_VERSION=""
if [[ -v ${CI_BRANCH} ]]; then
   BRANCH=${CI_BRANCH}
else
   BRANCH=`git rev-parse --abbrev-ref HEAD`
fi
LASTTAG=`git describe --abbrev=0 --tags`
case ${BRANCH:0:6} in
  releas)
     DISPLAY_VERSION=${LASTTAG}
     BRANCH=${BRANCH/release\//}
  ;;
  hotfix)
     DISPLAY_VERSION=${BRANCH}
     BRANCH=${BRANCH/hotfix\//}
  ;;
  featur)
     DISPLAY_VERSION=${BRANCH}
     BRANCH=${BRANCH/feature\//}
  ;;
  *)
     DISPLAY_VERSION=${LASTTAG}

     BRANCH=${BRANCH/release\//}
  ;;
esac
COMMIT=`git rev-parse HEAD`
DATE=`git log -1 --format=%cd`

V1=`echo $DATE | cut -d ' ' -f 5`
V2=`echo $DATE | cut -d ' ' -f 2`
V3=`echo $DATE | cut -d ' ' -f 3`
V4=`echo $DATE | cut -d ' ' -f 4`
VERSION=$V1.$V2.$V3.$V4

# write a json file
cat << EOF > $JSON
{
"commit" : "$COMMIT",
"branch" : "$BRANCH",
"date" : "$DATE",
"marketingVersion" : "$1",
"serial" : "$VERSION",
"tag" : "$LASTTAG",
"displayVersion" : "${DISPLAY_VERSION}"
}
EOF

# write a java properties file
cat << EOF > $PROPERTIES
commit="$COMMIT"
branch="$BRANCH"
date="$DATE"
marketingVersion="$1"
serial="$VERSION"
tag="$LASTTAG"
displayVersion="${DISPLAY_VERSION}"
EOF

# write a bash file
cat << EOF > $BASH
export COMMIT="$COMMIT"
export BRANCH="$BRANCH"
export DATE="$DATE"
export MARKETINGVERSION="$1"
export SERIAL="$VERSION"
export TAG="$LASTTAG"
export DISPLAY_VERSION="${DISPLAY_VERSION}"
EOF

# write version file for Registry
cat << EOF > $VERSH
$LASTTAG
EOF

# write version file for Installer Branding
cat << EOF > $VERSH2
!define sVersion $LASTTAG
EOF
