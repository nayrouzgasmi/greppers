package tn.esprit.pidev.Security.Payload.Request;

import lombok.Data;

@Data
public class ActiveAccount {
    private String mail;
    private String code;
}
