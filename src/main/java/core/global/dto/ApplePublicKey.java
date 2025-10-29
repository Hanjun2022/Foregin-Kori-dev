package core.global.dto;

import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public record ApplePublicKey(String kty, String kid,String use, String alg, String n, String e) {
}
