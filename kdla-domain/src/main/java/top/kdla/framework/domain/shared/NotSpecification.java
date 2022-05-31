package top.kdla.framework.domain.shared;

/**
 * NOT decorator, used to create a new specifcation that is the inverse (NOT) of the given spec.
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class NotSpecification<T> extends AbstractSpecification<T> {

  private Specification<T> spec1;

  /**
   * Create a new NOT specification based on another spec.
   *
   * @param spec1 Specification instance to not.
   */
  public NotSpecification(final Specification<T> spec1) {
    this.spec1 = spec1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSatisfiedBy(final T t) {
    return !spec1.isSatisfiedBy(t);
  }
}
