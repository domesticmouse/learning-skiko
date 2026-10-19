// Reusable Interactive Quiz Widget
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.quiz-container').forEach(container => {
    const options = container.querySelectorAll('.quiz-option');
    const feedback = container.querySelector('.quiz-feedback');

    options.forEach(option => {
      option.addEventListener('click', () => {
        // Clear previous states
        options.forEach(opt => {
          opt.classList.remove('correct', 'incorrect');
        });

        const isCorrect = option.getAttribute('data-correct') === 'true';
        if (isCorrect) {
          option.classList.add('correct');
          if (feedback) {
            feedback.style.display = 'block';
            feedback.style.backgroundColor = 'rgba(16, 185, 129, 0.15)';
            feedback.style.border = '1px solid var(--success)';
            feedback.style.color = '#a7f3d0';
            feedback.innerHTML = '<strong>Correct!</strong> ' + (option.getAttribute('data-explanation') || 'Well done.');
          }
        } else {
          option.classList.add('incorrect');
          if (feedback) {
            feedback.style.display = 'block';
            feedback.style.backgroundColor = 'rgba(239, 68, 68, 0.15)';
            feedback.style.border = '1px solid #ef4444';
            feedback.style.color = '#fecaca';
            feedback.innerHTML = '<strong>Not quite.</strong> ' + (option.getAttribute('data-explanation') || 'Review the section above and try again.');
          }
        }
      });
    });
  });
});
