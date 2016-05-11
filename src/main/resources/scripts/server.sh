#!/bin/sh

set -h -e

CLASSPATH=.:lib/*
OPT=-Dconf-file=config/conf-116.properties
java -server -Xms512M -Xmx1024M -Xss256k -XX:PermSize=256m -XX:MaxPermSize=256m $OPT -cp $CLASSPATH com.homethy.drip.mail.task.server.Bootstrap $@