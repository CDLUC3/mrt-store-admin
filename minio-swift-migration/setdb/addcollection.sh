dbtype=$1;
BASEDIR=$(dirname $0)
cd $BASEDIR
perl setreplic.pl ../conf/${dbtype}insert.prop
