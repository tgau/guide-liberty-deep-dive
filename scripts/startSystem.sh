#!/bin/bash

cd ./finsh/system
mvn clean package liberty:create liberty:install-feature liberty:deploy
mvn liberty:start