package vn.camerahub.identity;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class PrivateEvidenceStoreTest {
    @TempDir Path temp;
    @Test
    void refusesTraversalSpoofedAndOversizedFiles() {
        var store = new PrivateEvidenceStore(temp.toString());
        assertThatThrownBy(() -> store.resolve("../secret.pdf")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> store.put(new MockMultipartFile("file","fake.jpg","image/jpeg","bad".getBytes()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> store.put(new MockMultipartFile("file",new byte[5*1024*1024+1]))).isInstanceOf(ResponseStatusException.class);
    }
    @Test
    void createsOpaqueKeysAndCleansUpWithoutUsingClientFilenames() throws Exception {
        var store = new PrivateEvidenceStore(temp.toString());
        var bytes = "%PDF-1.4\nfixture".getBytes();
        String key = store.put(new MockMultipartFile("file","../../private.pdf","text/html",bytes));
        assertThat(key).matches("[a-f0-9-]{36}\\.pdf");
        assertThat(Files.readAllBytes(store.resolve(key))).isEqualTo(bytes);
        store.delete(key);
        assertThat(Files.exists(store.resolve(key))).isFalse();
    }
}
