package com.lingjoin.spring;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Member;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * 由于原有 UUIDGenerator 弃用, 复用 {@link org.hibernate.id.uuid.UuidGenerator}
 */
public class ProvidedUUIDGenerator extends org.hibernate.id.uuid.UuidGenerator {

    public ProvidedUUIDGenerator(ProvidedUuidGenerator config, Member idMember, CustomIdGeneratorCreationContext creationContext) {
        super(config.generator(), idMember, creationContext);
    }

    @IdGeneratorType( ProvidedUUIDGenerator.class )
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ FIELD, METHOD })
    public @interface ProvidedUuidGenerator{
        UuidGenerator generator();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        Object identifier = session.getEntityPersister(owner.getClass().getName(), owner).getIdentifier(owner, session);
        if( identifier == null){
            return super.generate(session, owner, null, eventType);
        }else {
            return identifier;
        }
    }
}
