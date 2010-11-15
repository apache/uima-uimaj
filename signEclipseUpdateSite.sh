#!/bin/sh

#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

# Bourne shell syntax, this should hopefully run on pretty much anything.

usage() {
  echo "Usage: cd to the uimaj-eclipse-update-site project, then ./signEclipseUpdateSite.sh"
}

if [ "$1" = "-help" ]
then
  usage
  exit 1
fi

# Create PGP signatures
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do gpg --output $i.asc --detach-sig --armor $i; done
for i in  target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do gpg --output $i.asc --detach-sig --armor $i; done
for i in  target/eclipse-update-site/plugins/org.apache.uima.*.pack.gz;  do gpg --output $i.asc --detach-sig --armor $i; done
gpg --output target/eclipse-update-site/digest.zip.asc --detach-sig --armor target/eclipse-update-site/digest.zip


# Create MD5 checksums
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do md5sum --binary $i > $i.md5; done
for i in  target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do md5sum --binary $i > $i.md5; done
md5sum --binary target/eclipse-update-site/digest.zip > target/eclipse-update-site/digest.zip.md5

# Create SHA1 checksums
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do sha1sum --binary $i > $i.sha1; done
for i in  target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do sha1sum --binary $i > $i.sha1; done
sha1sum --binary target/eclipse-update-site/digest.zip > target/eclipse-update-site/digest.zip.sha1
