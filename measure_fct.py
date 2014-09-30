#!/usr/bin/python

import hashlib
import pcap
import sys
import dpkt
import argparse

timestamps = {}
latencies = []
history = []

synPkts = 0
finPkts = 0
completeFlows = 0
lastHistoryUpdate = 0

def computeTime(key, ts):
    last_ts = timestamps[key]
    curr_ts = ts
#    print "%f,%s" % ((curr_ts - last_ts), key)
    latencies.append(curr_ts - last_ts)

def computeChange():
    global history
    offsets = [5,10,15,30,60]
    changes = []
    for i in offsets:
        if (len(history) > i):
            new = history[len(history)-1]
            orig = history[len(history)-i] 
            change = (new - orig) / orig
            changes.append(change)
        else:
            changes.append(-1)
    return changes

def updateHistory():
    global latencies
    global history
    global synPkts
    global finPkts
    global completeFlows

    mean = -1
    sumLat = sum(latencies)
    numLat = len(latencies)
    if (len(latencies) > 0):
        mean = sumLat / numLat
    history.append(mean)
   
    counts = [synPkts,finPkts,completeFlows]
    values = [sumLat,numLat,mean]
    changes = computeChange()
    print "%s,%s,%s" % (",".join("%d" % (num) for num in counts),",".join("%.6f" % (num) for num in values), ",".join("%.3f" % (num) for num in changes))
 
    latencies = []
    synPkts = 0
    finPkts = 0
    completeFlows = 0

def handlePacket(ts, pkt):
    global lastHistoryUpdate
    if (ts - lastHistoryUpdate >= 1):
        updateHistory()
        lastHistoryUpdate = ts

#    print " ".join("{:02x}".format(ord(c)) for c in pkt)
    ethPkt = dpkt.ethernet.Ethernet(pkt)
#    print ":".join("{:02x}".format(ord(c)) for c in ethPkt.src)
#    print ":".join("{:02x}".format(ord(c)) for c in ethPkt.dst)
#    print "0x%04x" % (ethPkt.type)
    ipPkt = dpkt.ip.IP(str(ethPkt.data))
    srcIp = ".".join("{:d}".format(ord(c)) for c in ipPkt.src)
    dstIp = ".".join("{:d}".format(ord(c)) for c in ipPkt.dst)
    tcpPkt = dpkt.tcp.TCP(str(ipPkt.data))
    srcPort = tcpPkt.sport
    dstPort = tcpPkt.dport
    key = "%s:%d-%s:%d" % (srcIp, srcPort, dstIp, dstPort)
    invKey = "%s:%d-%s:%d" % (dstIp, dstPort, srcIp, srcPort)

    finFlag = (tcpPkt.flags & dpkt.tcp.TH_FIN) != 0
    synFlag = (tcpPkt.flags & dpkt.tcp.TH_SYN) != 0
    ackFlag = (tcpPkt.flags & dpkt.tcp.TH_ACK) != 0

    if (synFlag and not ackFlag):
#        print "SYN %s" % (key)
        global synPkts
        synPkts+=1
        timestamps[key] = ts
    elif (finFlag):
#        print "FIN %s" % (invKey)
        global finPkts
        finPkts+=1
        if (invKey in timestamps):
            global completeFlows
            completeFlows+=1
            computeTime(invKey, ts)
            del timestamps[invKey]

def main():
    parser=argparse.ArgumentParser(description='Measure FCT')
    parser.add_argument('iface', metavar='iface', 
            help='Network interface')

    args = parser.parse_args()
    print "#%s" % (args)

    print "#iface %s" % (args.iface)

    capture = pcap.pcap(name=args.iface)
    capture.setfilter('tcp[tcpflags] & (tcp-syn|tcp-fin) != 0')

    capture.loop(handlePacket)

#    # Output statistics
#    print "#srcPkts\t%d" % (srcPkts)
#    print "#dstPkts\t%d" % (dstPkts)
#    print "#matchPkts\t%d" % (matchPkts)
#    print "#collidePkts\t%d" % (collidePkts)
#    print "#collideHashes\t%d" % (len(collide))
#    if(len(latencies) > 0):
#        print "#mean latency\t%f" % (sum(latencies) / len(latencies))
#        print "#min latency\t%f" % (min(latencies))
#        print "#max latency\t%f" % (max(latencies))

main()

