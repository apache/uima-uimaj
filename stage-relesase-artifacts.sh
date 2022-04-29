#!/bin/sh
set -e

if [ $# -eq 0 ] ; then
    echo "Supply two arguments: version and release candidate number, e.g."
    echo "  stage-release-artifacts 3.3.0 1"
    exit
fi

STAGING_DIR="./target/release-staging"
VERSION="$1"
RC="$2"

echo "Setting up local staging point at ${STAGING_DIR} (auto-deleted when script ends)"
trap 'rm -R "${STAGING_DIR}"' EXIT
mkdir "${STAGING_DIR}"

echo "Copying release artifacts to local staging spot"
for artifact in `find . -path "./target/checkout/target/uimaj-${VERSION}*" | grep -v 'pom'`
do
  cp ${artifact} ${STAGING_DIR}
done

echo "Copying Eclipse update site to local staging spot"
cp -r uimaj-eclipse-update-site/target/eclipse-update-site-v3 "${STAGING_DIR}"


echo "Uploading local staging spot into ASF subversion"
pushd "${STAGING_DIR}"
svn import . -m "Staging release artifacts for ${VERSION}-RC${RC}" "https://dist.apache.org/repos/dist/dev/uima/uima-uimaj-${VERSION}-RC${RC}"
popd

echo "Done"