# onos-app
## onos版本
version >= 2.2

onos-2.2 分支的 onos-create-app 工具有bug，创建出来的 onos-app 工程无法被onos加载

推荐使用 master 分支的 onos-create-app 工具，创建好之后，再切换到 onos-2.2

即：
```
cd ~/onos
git checkout master
cd ~
onos-create-app # 用master分支的onos-create-app工具
# 创建 app
cd ~/onos
git checkout onos-2.2 # 将版本切换为 onos-2.2.
bazel build onos
```

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
