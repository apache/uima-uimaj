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
  echo "Usage: cd to this project's project directory, then ./verifySigsEclipseUpdateSite.sh"
}

if [ "$1" = "-help" ]
then
  usage
  exit 1
fi

# Verify PGP signatures, MD5 and SHA1 checksums on all jars
for i in $(find ./target/eclipse-update-site -name '*.jar') 
  do 
    gpg --verify $i.asc
    md5sum --check $i.md5
    sha1sum --check $i.sha1
  done

# Verify PGP signatures, MD5 and SHA1 checksums on all gz files
for i in $(find ./target/eclipse-update-site -name '*.gz') 
  do 
    gpg --verify $i.asc
    md5sum --check $i.md5
    sha1sum --check $i.sha1
  done