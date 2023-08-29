package io.github.yangyaofei.spring;

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
 * 用于 UUID 类型在使用 @GeneratedValue 标注后即使设置了值还是会随机一个值的问题.
 * 复用 {@link org.hibernate.id.uuid.UuidGenerator}
 *
 * 使用方式:
 * <pre>{@code
 *     @Id
 *     @GeneratedValue
 *     @ProvidedUUIDGenerator.ProvidedUuidGenerator(generator = @UuidGenerator)
 *     private UUID userId;
 * }</pre>
 */
public class ProvidedUUIDGenerator extends org.hibernate.id.uuid.UuidGenerator {

    @SuppressWarnings("MissingJavadoc")
    public ProvidedUUIDGenerator(ProvidedUuidGenerator config, Member idMember, CustomIdGeneratorCreationContext creationContext) {
        super(config.generator(), idMember, creationContext);
    }

    /**
     * Annotation for {@link ProvidedUUIDGenerator}
     */
    @IdGeneratorType(ProvidedUUIDGenerator.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({FIELD, METHOD})
    public @interface ProvidedUuidGenerator {
        /**
         *  {@link org.hibernate.id.uuid.UuidGenerator}
         *
         * @return uuid generator
         */
        UuidGenerator generator();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        Object identifier = session.getEntityPersister(owner.getClass().getName(), owner).getIdentifier(owner, session);
        if (identifier == null) {
            return super.generate(session, owner, null, eventType);
        } else {
            return identifier;
        }
    }
}
