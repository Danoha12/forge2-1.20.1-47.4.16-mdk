package com.trolmastercard.sexmod.network;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;

import java.net.SocketAddress;

/**
 * FakeNetworkManager - ported from x.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A stub {@link Connection} (formerly {@code NetworkManager}) used when the
 * mod needs to simulate a network connection locally - e.g., for spawning
 * fake player connections in dev/admin tools.
 *
 * All Channel methods return null / false / 0. The {@link FakeChannel#attr}
 * method returns a no-op {@link Attribute} so that GeckoLib / Netty code
 * that reads attributes doesn't NPE.
 *
 * In 1.20.1 {@code NetworkManager} was renamed to {@link Connection} and
 * takes a {@link PacketFlow} enum instead of {@code EnumPacketDirection}.
 */
public class FakeNetworkManager extends Connection {

    public FakeNetworkManager(PacketFlow flow) {
        super(flow);
    }

    /** Returns a stub {@link Channel} whose methods are all no-ops. */
    @Override
    public Channel channel() {
        return new FakeChannel();
    }

    // =========================================================================
    //  Stub Channel
    // =========================================================================

    static final class FakeChannel implements Channel {

        @Override public ChannelId      id()              { return null; }
        @Override public EventLoop      eventLoop()       { return null; }
        @Override public Channel        parent()          { return null; }
        @Override public ChannelConfig  config()          { return null; }
        @Override public boolean        isOpen()          { return false; }
        @Override public boolean        isRegistered()    { return false; }
        @Override public boolean        isActive()        { return false; }
        @Override public ChannelMetadata metadata()       { return null; }
        @Override public SocketAddress  localAddress()    { return null; }
        @Override public SocketAddress  remoteAddress()   { return null; }
        @Override public ChannelFuture  closeFuture()     { return null; }
        @Override public boolean        isWritable()      { return false; }
        @Override public long           bytesBeforeUnwritable() { return 0L; }
        @Override public long           bytesBeforeWritable()   { return 0L; }
        @Override public Unsafe         unsafe()          { return null; }
        @Override public ChannelPipeline pipeline()       { return null; }
        @Override public ByteBufAllocator alloc()         { return null; }
        @Override public ChannelPromise newPromise()      { return null; }
        @Override public ChannelProgressivePromise newProgressivePromise() { return null; }
        @Override public ChannelFuture  newSucceededFuture()               { return null; }
        @Override public ChannelFuture  newFailedFuture(Throwable c)       { return null; }
        @Override public ChannelPromise voidPromise()     { return null; }
        @Override public ChannelFuture  bind(SocketAddress a)              { return null; }
        @Override public ChannelFuture  connect(SocketAddress a)           { return null; }
        @Override public ChannelFuture  connect(SocketAddress a, SocketAddress b) { return null; }
        @Override public ChannelFuture  disconnect()      { return null; }
        @Override public ChannelFuture  close()           { return null; }
        @Override public ChannelFuture  deregister()      { return null; }
        @Override public ChannelFuture  bind(SocketAddress a, ChannelPromise p)            { return null; }
        @Override public ChannelFuture  connect(SocketAddress a, ChannelPromise p)         { return null; }
        @Override public ChannelFuture  connect(SocketAddress a, SocketAddress b, ChannelPromise p) { return null; }
        @Override public ChannelFuture  disconnect(ChannelPromise p)       { return null; }
        @Override public ChannelFuture  close(ChannelPromise p)            { return null; }
        @Override public ChannelFuture  deregister(ChannelPromise p)       { return null; }
        @Override public Channel        read()            { return null; }
        @Override public ChannelFuture  write(Object m)                    { return null; }
        @Override public ChannelFuture  write(Object m, ChannelPromise p)  { return null; }
        @Override public Channel        flush()           { return null; }
        @Override public ChannelFuture  writeAndFlush(Object m, ChannelPromise p) { return null; }
        @Override public ChannelFuture  writeAndFlush(Object m)            { return null; }
        @Override public int            compareTo(Channel o)               { return 0; }

        /** Returns a no-op attribute so attr() calls never NPE. */
        @Override
        public <T> Attribute<T> attr(AttributeKey<T> key) {
            return new NoOpAttribute<>();
        }

        @Override
        public <T> boolean hasAttr(AttributeKey<T> key) {
            return false;
        }

        // =====================================================================
        //  No-op Attribute implementation
        // =====================================================================

        private static final class NoOpAttribute<T> implements Attribute<T> {
            @Override public AttributeKey<T> key()                       { return null; }
            @Override public T               get()                       { return null; }
            @Override public void            set(T value)                {}
            @Override public T               getAndSet(T value)          { return null; }
            @Override public T               setIfAbsent(T value)        { return null; }
            @Override public T               getAndRemove()              { return null; }
            @Override public boolean         compareAndSet(T o, T n)     { return false; }
            @Override public void            remove()                    {}
        }
    }
}
