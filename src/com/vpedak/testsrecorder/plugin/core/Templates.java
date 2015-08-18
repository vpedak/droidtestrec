package com.vpedak.testsrecorder.plugin.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Templates {
    private static Templates instance = new Templates();
    private Map<String, String> templates = new HashMap<String, String>();

    private Templates() {
    }

    public static Templates getInstance() {
        return instance;
    }

    public String getTemplate(String name) {
        String res = templates.get(name);
        if (res == null) {
            res = loadTemplate(name);
            templates.put(name, res);
        }

        return res;
    }

    private String loadTemplate(String name) {
        StringBuilder build = new StringBuilder();
        byte[] buf = new byte[1024];
        InputStream is = getClass().getResourceAsStream("template/"+name+".txt");
        try {
            int length;
            while ((length = is.read(buf)) != -1) {
                build.append(new String(buf, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
        return build.toString();
    }

}
