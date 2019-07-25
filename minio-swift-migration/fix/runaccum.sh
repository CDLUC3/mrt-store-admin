accver=$1
DATE=`date '+%Y%m%d-%H%M%S'`
chmod 755 ./in/accum.${accver}.sh
./in/accum.${accver}.sh > ./out/out-accum.${accver}.${DATE}.txt &
