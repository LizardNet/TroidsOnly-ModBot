package com.troidsonly.modbot.commands.mute;

import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;

public class MuteListener extends ListenerAdapter {

    private final AccessControl acl;
    private final PersistenceManager<MuteConfig> pm;
    private final MuteConfig config;

    private final MuteHandler commandHandler;

    public MuteListener(AccessControl acl, PersistenceWrapper<?> wrapper) {
        this.acl = acl;
        // Old name preserved as namespace for backwards comaptibility
        pm = wrapper.getPersistenceManager("CryoConfig", MuteConfig.class);

        config = pm.get().orElseGet(MuteConfig::empty);

        commandHandler = new MuteHandler(this);
    }

    public MuteHandler getCommandHandler() {
        return commandHandler;
    }

    AccessControl getAcl() {
        return acl;
    }

    PersistenceManager<MuteConfig> getPm() {
        return pm;
    }

    MuteConfig getConfig() {
        return config;
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    public String getMuteRoleId() {
        return config.getMuteRoleId();
    }
}
