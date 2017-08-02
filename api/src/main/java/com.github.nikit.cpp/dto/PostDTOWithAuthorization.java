package com.github.nikit.cpp.dto;

import java.net.URL;

public class PostDTOWithAuthorization extends PostDTO {
    private UserAccountDTO owner;
    private boolean canEdit;
    private boolean canDelete;

    public PostDTOWithAuthorization(
            long id,
            String title,
            String text,
            URL titleImg,
            UserAccountDTO userAccountDTO,
            boolean canEdit,
            boolean canDelete
    ) {
        super(id, title, text, titleImg);
        this.owner = userAccountDTO;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }


    public UserAccountDTO getOwner() {
        return owner;
    }

    public void setOwner(UserAccountDTO owner) {
        this.owner = owner;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}