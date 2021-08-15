package com.ariseontech.joindesk.auth.util;

public class View {
    public interface Public {
    }

    public interface Details extends Public {
    }

    public interface Report extends Public {
    }

    public interface Admin extends Details {
    }
}