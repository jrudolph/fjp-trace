/*
 * Copyright (c) 2012 Aleksey Shipilev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shipilev.fjptrace.tasks;

import java.io.PrintWriter;
import java.util.concurrent.RecursiveAction;

public abstract class LoggedRecursiveAction extends RecursiveAction {

    private final String description;
    private final PrintWriter pw;

    public LoggedRecursiveAction(String description) {
        super();
        this.description = description;
        this.pw = new PrintWriter(System.out, true);
    }

    @Override
    protected void compute() {
        try {
            reportProgress(0);
            doWork();
            reportProgress(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PrintWriter getPw() {
        return pw;
    }

    protected void reportProgress(double fraction) {
        pw.printf("%9.2f%%: %s\n", fraction*100, description);
    }

    abstract void doWork() throws Exception;
}
