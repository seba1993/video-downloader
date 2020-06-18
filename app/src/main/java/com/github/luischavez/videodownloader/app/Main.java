package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.BaseContext;
import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.Manager;
import com.github.luischavez.videodownloader.manager.ManagerListener;
import com.github.luischavez.videodownloader.manager.SingleThreadManager;
import com.github.luischavez.videodownloader.system.DefaultSystem;
import com.github.luischavez.videodownloader.system.GuiceDependencyInjection;
import com.github.luischavez.videodownloader.system.Injected;
import com.github.luischavez.videodownloader.system.System;

public class Main {

    public static void main(String... args) throws Exception {
        final System system = new DefaultSystem(new GuiceDependencyInjection());

        system.getDependencyInjection().configure(dependencyRegister -> {
            dependencyRegister.bind(System.class, system);
            dependencyRegister.single(Context.class, DefaultConext.class);
            dependencyRegister.single(IFoo.class, FooA.class);
            //dependencyRegister.single(DemoManager.class, DemoManager.class);
        });

        IFoo foo = system.getDependencyInjection().make(IFoo.class);
        foo.sayHello();

        system.registerManager(DemoManager.class);

        DemoManager demoManager = system.getManager(DemoManager.class);
        demoManager.start();
    }

    private static class DefaultConext extends BaseContext implements ManagerListener {

        @Injected
        public DefaultConext(System system) {
            super(system);
        }

        @Override
        public void onManagerStart(Manager manager) {
            java.lang.System.out.println("started");
        }

        @Override
        public void onManagerStop(Manager manager) {
            java.lang.System.out.println("stopped");
        }

        @Override
        public void onManagerExecutionException(Manager manager, String message, Throwable throwable) {

        }
    }

    private static class DemoManager extends SingleThreadManager {

        @Injected
        public DemoManager(Context context) {
            super(context);
        }

        @Override
        protected boolean doWork() throws Exception {
            java.lang.System.out.println("test from manager");
            java.lang.System.out.println(getWorkingDir());
            return false;
        }
    }

    private interface IFoo {

        void sayHello();
    }

    private static class FooA implements IFoo {

        public void sayHello() {
            java.lang.System.out.println("hello from foo a");
        }
    }

    private static class FooB implements IFoo {

        public void sayHello() {
            java.lang.System.out.println("hello from foo b");
        }
    }
}
