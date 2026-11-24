package top.kdla.framework.supplement.trdcloud.server;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroovyShellCalSandboxTest {

    private void assertBlocked(String code) {
        // Groovy 将沙箱 SecurityException 包装为编译错误抛出，栈中含 SecurityException 即证明被沙箱拦截
        assertThatThrownBy(() -> TrdPlatformCloudServerImpl.groovyShellCal(code, new HashMap<>()))
                .isInstanceOf(MultipleCompilationErrorsException.class)
                .hasStackTraceContaining("java.lang.SecurityException");
    }

    @Test
    void commandExecutionBlocked() {
        assertBlocked("\"id\".execute()");
        assertBlocked("['sh', '-c', 'id'].execute()");
        assertBlocked("Runtime.getRuntime().exec('id')");
        assertBlocked("new ProcessBuilder(['id']).start()");
    }

    @Test
    void systemAndRuntimeAccessBlocked() {
        assertBlocked("System.exit(0)");
        assertBlocked("System.getenv()");
        assertBlocked("System.env");
        assertBlocked("System.out.println('x')");
        assertBlocked("Runtime.runtime");
    }

    @Test
    void fileAndNetworkBlocked() {
        assertBlocked("new File('/etc/passwd').text");
        assertBlocked("new URL('http://evil.example').text");
        assertBlocked("new Socket('evil.example', 80)");
        assertBlocked("java.nio.file.Files.readString(java.nio.file.Paths.get('/etc/passwd'))");
    }

    @Test
    void reflectionAndClassLoaderBlocked() {
        assertBlocked("Class.forName('java.lang.Runtime')");
        assertBlocked("'x'.getClass().forName('java.lang.Runtime')");
        assertBlocked("Thread.currentThread().getContextClassLoader()");
        assertBlocked("Eval.me('1+1')");
        assertBlocked("new GroovyShell().evaluate('1')");
    }

    @Test
    void explicitImportBlocked() {
        assertBlocked("import java.io.File\nnew File('/tmp')");
    }

    @Test
    void dosBlocked() {
        assertBlocked("Thread.sleep(60000)");
    }

    @Test
    void legitArithmeticStillWorks() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        assertThat(TrdPlatformCloudServerImpl.groovyShellCal("a + b", vars)).isEqualTo(3);
    }

    @Test
    void legitJsonParsingStillWorks() {
        JSONObject data = new JSONObject();
        data.put("total", 5);
        Map<String, Object> vars = new HashMap<>();
        vars.put("data", data);
        assertThat(TrdPlatformCloudServerImpl.groovyShellCal("data.total", vars)).isEqualTo(5);
        assertThat(TrdPlatformCloudServerImpl.groovyShellCal("data.getInteger('total')", vars)).isEqualTo(5);
    }

    @Test
    void legitCollectionOpsStillWorks() {
        JSONObject data = new JSONObject();
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("num", 2);
        items.add(item);
        data.put("items", items);
        Map<String, Object> vars = new HashMap<>();
        vars.put("data", data);
        assertThat(TrdPlatformCloudServerImpl.groovyShellCal("data.items.collect { it.num }.sum()", vars)).isEqualTo(2);
        assertThat(TrdPlatformCloudServerImpl.groovyShellCal("data.items.size()", vars)).isEqualTo(1);
    }
}
