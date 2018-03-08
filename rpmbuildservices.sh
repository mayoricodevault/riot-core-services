#!/bin/bash -x
NAME=vizix-core-services
RPMBUILD_DIR=rpm-builddir-services
SPEC_FILE=vizix-core-services.spec
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source build/tar/riot-core-services/git.sh

rm -rf $RPMBUILD_DIR
rm -rf vizix-services-*.rpm
mkdir -p $RPMBUILD_DIR/{RPMS,SOURCES,SPECS,SRPMS} #create the build directory structure
ln -s $DIR/build/tar/riot-core-services $RPMBUILD_DIR/BUILD
cp $SPEC_FILE $RPMBUILD_DIR/SPECS/
rpmbuild --define "_topdir $DIR/$RPMBUILD_DIR" -bb $RPMBUILD_DIR/SPECS/$SPEC_FILE
mv $RPMBUILD_DIR/RPMS/x86_64/*.rpm $DIR/
