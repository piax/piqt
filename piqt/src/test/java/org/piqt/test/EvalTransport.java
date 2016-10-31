package org.piqt.test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.piax.common.Endpoint;
import org.piax.common.ObjectId;
import org.piax.common.PeerLocator;
import org.piax.common.TransportId;
import org.piax.gtrans.ChannelTransport;
import org.piax.gtrans.IdConflictException;
import org.piax.gtrans.util.ThroughTransport;
import org.piax.pubsub.stla.ClusterId;

public class EvalTransport<E extends PeerLocator> extends ThroughTransport<E> {

    public int counter = 0;
    static ConcurrentHashMap<Endpoint, ClusterId> table = new ConcurrentHashMap<Endpoint, ClusterId>();

    public EvalTransport(ChannelTransport<E> trans, ClusterId cid)
            throws IdConflictException {
        super(new TransportId("c"), trans);
        table.put(trans.getEndpoint(), cid);
    }

    public EvalTransport(TransportId transId, ChannelTransport<E> trans)
            throws IdConflictException {
        super(transId, trans);
    }

    public void clearCounter() {
        synchronized (this) {
            counter = 0;
        }
    }

    public int getCounter() {
        int ret;
        synchronized (this) {
            ret = counter;
        }
        return ret;
    }

    @Override
    protected Object _preSend(ObjectId sender, ObjectId receiver, E dst,
            Object msg) throws IOException {
        synchronized (this) {
            int dist = table.get(getEndpoint()).distance(table.get(dst));
            try {
                Thread.sleep(dist * 10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
            // System.out.println("dist=" + dist);
            counter++;
        }
        return msg;
    }

    @Override
    protected Object _postReceive(ObjectId sender, ObjectId receiver, E src,
            Object msg) {
        return msg;
    }
}
