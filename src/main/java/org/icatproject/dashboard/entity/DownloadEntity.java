/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.dashboard.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlTransient;



@Comment("The mapping between a entity that can be associated with a download.")
@SuppressWarnings("serial")
@Entity

public class DownloadEntity extends EntityBaseBean implements Serializable {
            
    @JoinColumn(name="DOWNLOAD_ID",nullable=false)    
    @ManyToOne(fetch = FetchType.LAZY)
    private Download download;

    @JoinColumn(name="ENTITY_ID",nullable = false)   
    @ManyToOne(fetch = FetchType.LAZY)
     private Entity_ entity;
    
    public DownloadEntity(){
        
    }

    public void setDownload(Download download) {
        this.download = download;
    }

    public void setEntity(Entity_ entity) {
        this.entity = entity;
    }
    
    public Download getDownload() {
        return download;
    }

    @XmlTransient
    public Entity_ getEntity() {
        return entity;
    }
    
    
    
    
}
