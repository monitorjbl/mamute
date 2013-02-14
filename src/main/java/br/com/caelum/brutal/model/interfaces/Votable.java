package br.com.caelum.brutal.model.interfaces;

import java.io.Serializable;

import br.com.caelum.brutal.model.User;
import br.com.caelum.brutal.model.Vote;

public interface Votable {

	void substitute(Vote previous, Vote current);
	User getAuthor();
	Serializable getId();
	long getVoteCount();
}