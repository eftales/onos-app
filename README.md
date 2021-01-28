# onos-app
## onos版本
version >= 2.5

version<2.5 的 onos-create-app 工具有bug，创建出来的 onos-app 工程无法被onos加载

```
cd ~/onos
git checkout onos-2.5 # 将版本切换为 onos-2.5
cd ~
onos-create-app 
bazel build onos
```

如果非要用onos低版本的话，那就onos/apps文件夹下的原生onos应用，偷梁换柱一下

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
