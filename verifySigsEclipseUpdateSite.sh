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

# On windows, run this inside cygwin
# Bourne shell syntax, this should hopefully run on pretty much anything.

usage() {
  echo "Usage: cd to eclipseUpdateSite, then verifySigsEclipseUpdateSite.sh )"
}

# Verify PGP signatures
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do gpg --verify $i.asc; done
for i in target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do gpg --verify $i.asc; done
gpg --verify target/eclipse-update-site/digest.zip.asc

# Verify MD5 checksums
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do md5sum --check $i.md5; done
for i in target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do md5sum --check $i.md5; done
md5sum --check target/eclipse-update-site/digest.zip.md5

# Verify SHA1 checksums
for i in target/eclipse-update-site/features/org.apache.uima.*.jar; do sha1sum --check $i.sha1; done
for i in target/eclipse-update-site/plugins/org.apache.uima.*.jar;  do sha1sum --check $i.sha1; done
md5sum --check target/eclipse-update-site/digest.zip.md5

