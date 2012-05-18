#!/bin/sh
if [ $# -ne 5 ]
then
echo "usage: mkldif <devicename> <app-name> <host> <port> <proxy-port>"
exit 1
fi
echo "version: 1

dn: dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomDeviceName: $1
dicomInstalled: TRUE
objectClass: dicomDevice

dn: cn=hl7,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
cn: dicom
dicomHostname: $3
dicomPort: $4
objectClass: dicomNetworkConnection

dn: cn=proxy,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
cn: proxy
dicomHostname: proxy
dicomPort: $5
objectClass: dicomNetworkConnection

dn: hl7ApplicationName=$2,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
hl7ApplicationName: $2
dicomNetworkConnectionReference: cn=hl7,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
objectClass: hl7Application"
