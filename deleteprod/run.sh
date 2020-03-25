#!/bin/bash
out=$1
DATE=`date +%Y%m%d-%H%M`
pwd=`pwd`
echo "pwd=$pwd"
infile="$pwd/in/$out.txt"
report="$pwd/out/report-$out-$DATE.txt"
echo "perl ./calldel.pl $infile  $report"
perl ./calldel.pl $infile $report
