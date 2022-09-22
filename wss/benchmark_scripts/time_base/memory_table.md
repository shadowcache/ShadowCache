# how to set memeory
tail -n +2 bmc-twitter-120000m-87k-64mb-8-32.csv  | awk  '{maxSize=(maxSize<$2?$2:maxSize)} END{print maxSize}'

得到峰值对象个数N，这里向上取2的幂次

msr * 20
twitter * 22

得到内存大小

然后/1024
最后/8
得到kb

# msr
prxy_1:
    1h          160kb
    12h         320kb

mds_1
    1h:         320kb

usr_1:
    1h:         20mb

prn_1:
    1h:         5mb    

# twitter
    1h          11mb (bmc 1000%are)
    

