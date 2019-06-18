confname=$1
echo confname=$confname
BASEDIR=$(dirname $0)
cd $BASEDIR
perl report-generic.pl  "../conf/${confname}all.prop"
