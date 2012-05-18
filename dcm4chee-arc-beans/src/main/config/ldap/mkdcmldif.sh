#!/bin/sh
if [ $# -ne 5 ]
then
echo "usage: mkldif <devicename> <ae-title> <host> <port> <proxy-port>"
exit 1
fi
echo "version: 1

dn: dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomInstalled: TRUE
dicomDeviceName: $1
objectClass: dicomDevice

dn: cn=dicom,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomHostname: $3
dicomPort: $4
objectClass: dicomNetworkConnection
cn: dicom

dn: cn=proxy,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomHostname: proxy
dicomPort: $5
objectClass: dicomNetworkConnection
cn: proxy

dn: dicomAETitle=$2,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomNetworkConnectionReference: cn=dicom,dicomDeviceName=$1,cn=Devices,cn=DICOM Configuration,dc=nodomain
dicomAssociationAcceptor: TRUE
dicomAETitle: $2
objectClass: dicomNetworkAE"
