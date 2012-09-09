package hudson.plugins.clearcase.viewstorage;


class ServerViewStorage implements ViewStorage {

    private boolean auto;

    private String  server;

    ServerViewStorage() {
        this.auto = true;
    }

    ServerViewStorage(String server) {
        this.server = server;
    }

    @Override
    public String[] getCommandArguments() {
        if (auto) {
            return new String[] { "-stgloc", "-auto" };
        } else {
            return new String[] { "-stgloc", server };
        }
    }

    @Override
    public String getType() {
        return "server";
    }

}
