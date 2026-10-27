package vn.camerahub.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import vn.camerahub.identity.entity.Account;
import vn.camerahub.identity.mapper.ProfileMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMapperTest {
    @Test
    void accountProjectionsKeepRolesAndPublicVersion() {
        var account = new Account();
        account.setId(UUID.randomUUID());
        account.setFullName("Nguyễn An");
        account.setEmail("an@example.test");
        account.setPasswordHash("private-hash");
        account.setAccountStatus("ACTIVE");
        account.setRoles(List.of("USER", "ADMIN"));
        account.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        account.setProfileVersion(3L);
        var mapper = Mappers.getMapper(ProfileMapper.class);
        assertThat(mapper.toResponse(account).roles()).containsExactly("USER", "ADMIN");
        assertThat(mapper.toResponse(account).status()).isEqualTo("ACTIVE");
        assertThat(mapper.toPublicResponse(account).version()).isEqualTo(3);
        assertThat(mapper.toPublicResponse(account).joinedAt()).isEqualTo(account.getCreatedAt());
        assertThat(mapper.toResponse(account).toString()).doesNotContain("private-hash");
    }
}
