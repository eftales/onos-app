# onos-app
## onos版本
version >= 2.5

现在是2021年1月27日，我使用master分支做实验没有问题，如果使用 onos-2.2 分支做实验，就一堆bug...

TODO: 康康onos2.3是否可以

## how to compile
```
mvn compile
mvn clean install
```

执行完成之后会在 target/ 文件夹下生成 .oar 文件

## how to install 
```
cd ~/onos
bazel run onos-local -- clean debug

cd ~/onos-app
onos-app localhost install target/onos-app-1.0-SNAPSHOT.oar

cd ~/onos
tools/test/bin/onos localhost
app activate onos-app
app deactivate org.onosproject.fwd 
```

## how to test
After you installed onos-app in ONOS:

```
sudo mn --controller remote,ip=127.0.0.1 --topo minimal
pingall
```

## how to debug
https://blog.csdn.net/fsdgfsf/article/details/90369709

https://www.sdnlab.com/15197.html
