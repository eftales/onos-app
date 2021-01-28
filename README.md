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

## 技术难点
1. 如何通过 lsc 标签进行转发

    当前私有协议转发是通过给交换机添加 mac 匹配项实现的

    `selectBuilder.matchEthSrc(inPkt.getSourceMAC()).matchEthDst(inPkt.getDestinationMAC())`

    如果要匹配自定义字段，则需要修改 maven 依赖、onos源码、ovs、p4

2. onos-app 如何得到 lsc 列表

    activate() 的时候获取最初的 lsc 列表

    之后通过特定的 packet_in 消息更新 lsc 列表

3. 如何通过 lsc 得到 host 的地址

    onos只可以通过mac得到host的位置 `HostId.hostId(ethPkt.getDestinationMAC())`

    如果运行私有协议之后，mac地址变为随机值，那么onos-app就需要存储 lsc 和 mac 的映射,才能得到 host 的位置

    host向onos请求(packet_in)lsc时必须使用真实的mac地址


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
