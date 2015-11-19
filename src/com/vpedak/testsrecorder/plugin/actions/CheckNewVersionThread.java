package com.vpedak.testsrecorder.plugin.actions;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.vpedak.testsrecorder.plugin.ui.EventsList;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class CheckNewVersionThread extends Thread {
    private ToolsTestsRecorderAction recorderAction;

    public CheckNewVersionThread(ToolsTestsRecorderAction recorderAction) {
        this.recorderAction = recorderAction;
    }

    @Override
    public void run() {
        String line = null;
        BufferedReader reader = null;

        try {
            URL url = new URL("http://droidtestlab.com/check_version.html");
            InputStream inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String str = reader.readLine();
            while (str != null) {
                if (str.startsWith("last version:")) {
                    line = str.substring(14).trim();
                    break;
                }
                str = reader.readLine();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (line != null && line.length() > 0) {
            String current = PluginManager.getPlugin(PluginId.getId("com.vpedak.testsrecorder.plugin.id")).getVersion();

            if (!current.equals(line)) {
                final String finalLine = line;
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        recorderAction.showNewVersionAvailable(finalLine);
                    }
                });
            }
        }
    }
}
