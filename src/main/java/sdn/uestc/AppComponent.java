/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdn.uestc;


import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {AppComponent.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    private String someProperty;

    //-> add 1 成员变量

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService; // 由外界注入

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    protected ApplicationId appId;

    private PacketProcessor packetProcessor;


    //<- add 1 end

    @Activate
    protected void activate() {


        //-> add 2

        appId = coreService.registerApplication("net.fnlab.fwd");
        packetProcessor = new ReactivePacketProcessor();
        packetService.addProcessor(packetProcessor, PacketProcessor.director(2));

        requestIntercepts();
        //<- add 2 end
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        withdrawIntercepts();
        packetService.removeProcessor(packetProcessor);
        flowRuleService.removeFlowRulesById(appId);
        packetProcessor = null;
        log.info("Stopped");
    }


    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

    }

    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();

        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    private void installRule(PacketContext context, PortNumber portNumber) {
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectBuilder = DefaultTrafficSelector.builder();
        selectBuilder.matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectBuilder.build())
                .withTreatment(treatment)
                .withPriority(10)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(10)
                .add();
        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                forwardingObjective);
        //Send packet out to the specified port
        packetOut(context, portNumber);
    }


    public class ReactivePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (context.isHandled()) {
                return;
            }

            if (isControlPacket(ethPkt)) {
                return;
            }

            if (ethPkt.isMulticast()) {
                return;
            }

            HostId dstHostId = HostId.hostId(ethPkt.getDestinationMAC());
            Host dstHost = hostService.getHost(dstHostId);

            if (null == dstHost) {
                flood(context);
                return;
            }

            // 发送方和接收方在一个交换机上
            if (pkt.receivedFrom().deviceId().equals(dstHost.location().deviceId())) {
                installRule(context, dstHost.location().port());
                return;
            }

            Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(),
                        pkt.receivedFrom().deviceId(),
                        dstHost.location().deviceId());

            if (paths.isEmpty()) {
                flood(context);
                return;
            }

            Path path = paths.iterator().next();
            if (null == path) {
                flood(context);
                return;
            }

            installRule(context, path.src().port());

        }

    }
}

